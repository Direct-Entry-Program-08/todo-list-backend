package lk.ijse.dep8.todo.api;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import lk.ijse.dep8.todo.dto.ItemDTO;
import lk.ijse.dep8.todo.dto.UserDTO;
import lk.ijse.dep8.todo.exception.ValidationException;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "ItemServlet", value = "/items/*")
public class ItemServlet extends HttpServlet {
    @Resource(name = "java:comp/env/jdbc/pool4todo")
    private volatile DataSource pool;
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
       doSaveOrUpdate(req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doSaveOrUpdate(req, resp);
    }

    private void doSaveOrUpdate(HttpServletRequest request , HttpServletResponse response) throws IOException {

        /*Basically POST and PUT requests have json body*/
        if(request.getContentType() == null || !request.getContentType().toLowerCase().startsWith("application/json")){
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Required content type is not available");
            return;
        }

        String method = request.getMethod();
        String pathInfo = request.getPathInfo();

        if(method.equals("POST") && !(request.getServletPath().equalsIgnoreCase("/items") || request.getServletPath().equalsIgnoreCase("/items/"))){
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid url pattern");
            return;
        }else if(method.equals("PUT") && !(pathInfo != null && pathInfo.substring(1).matches("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"))){
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found in the DB");
            return;
        }

        try{
            Jsonb jsonb = JsonbBuilder.create();
            ItemDTO item = jsonb.fromJson(request.getReader(), ItemDTO.class);

            /*Validate the user details*/
            if(method.equals("POST") && (item.getUserEmail() == null || !item.getUserEmail().matches("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"))){
                throw new ValidationException("Invalid Email");
            }else if(item.getDescription() == null ){
                throw new ValidationException("Empty body");
            }else if(item.getStatus() == null ){
                throw new ValidationException("Empty state");
            }

            if (method.equals("PUT")){
                item.setUserEmail(pathInfo.replaceAll("[/]", ""));
            }

            /*Create a connection with DB*/
            try(Connection connection = pool.getConnection()){
                PreparedStatement stm = connection.prepareStatement("SELECT * FROM Item WHERE user_mail=? ");
                stm.setString(1, item.getUserEmail());
                ResultSet results = stm.executeQuery();

                /*Save or Update logic*/
                if(results.next()){
                    if(method.equals("POST")){

                        response.sendError(HttpServletResponse.SC_CONFLICT, "This user already saved in the DB");
                    }else{

                        PreparedStatement stm1 = connection.prepareStatement("UPDATE Item SET description=?, status=? WHERE user_mail=?");
                        stm1.setString(1, item.getDescription());
                        stm1.setString(2, item.getStatus());
                        stm1.setString(3, item.getUserEmail());
                        int updatedResults = stm1.executeUpdate();
                        if(updatedResults != 1){
                            throw new RuntimeException("Failed to Update user");
                        }
                        response.setStatus(HttpServletResponse.SC_CREATED);
                    }
                }else{

                    PreparedStatement stm3 = connection.prepareStatement("INSERT INTO Item (description, status, user_mail) VALUES (?,?,?)");
                    stm3.setString(1, item.getDescription());
                    stm3.setString(2, item.getStatus());
                    stm3.setString(3, item.getUserEmail());
                    int savedResults = stm3.executeUpdate();
                    if(savedResults != 1){
                        throw new RuntimeException("Failed to Save user");
                    }
                    response.setStatus(HttpServletResponse.SC_CREATED);
                }
            }
        }catch (JsonbException | ValidationException e){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, (e instanceof JsonbException)? "Invalid Json":e.getMessage());
        }catch (Throwable t){
            t.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (request.getPathInfo() == null || request.getPathInfo().equals('/')){
            response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Unable to delete all Items yet");
            return;
        }else if (request.getPathInfo() != null && !request.getPathInfo().substring(1).matches("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")){
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found");
            return;
        }

        String user_email = request.getPathInfo().replaceAll("[/]", "");

        try(Connection connection = pool.getConnection()){
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM Item WHERE user_mail=?");
            stm.setString(1, user_email);
            ResultSet findResults = stm.executeQuery();
            if(findResults.next()){
                stm = connection.prepareStatement("DELETE FROM Item WHERE user_mail=?");
                stm.setString(1, user_email);
                int deleteResults = stm.executeUpdate();
                if(deleteResults != 1){
                    throw new RuntimeException("Failed to Delete User");
                }else{
                    response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                }
            }else{
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        }catch (ValidationException e){
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }catch (Throwable t){
            t.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if(request.getPathInfo() != null && !request.getPathInfo().equals("/")){
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String query = request.getParameter("q");
        query = "%" + (query==null?"":query) + "%";

        try(Connection connection = pool.getConnection()){

            /*Consider the pagination*/
            boolean pagination = request.getParameter("page") != null && request.getParameter("size") != null;
            String sql = null;
            if(pagination){
                sql = "SELECT * FROM Item WHERE user_mail LIKE ? OR description LIKE ? LIMIT ? OFFSET ?";
            }else {
                sql = "SELECT * FROM Item WHERE user_mail LIKE ? OR description LIKE ?";
            }

            PreparedStatement stm = connection.prepareStatement(sql);
            PreparedStatement stmCount = connection.prepareStatement("SELECT count(*) FROM Item WHERE user_mail LIKE ? OR description LIKE ?");
            stm.setString(1, query);
            stm.setString(2, query);
            stmCount.setString(1, query);
            stmCount.setString(2, query);

            if(pagination){
                int page = Integer.parseInt(request.getParameter("page"));
                int size = Integer.parseInt(request.getParameter("size"));
                stm.setInt(4, size);
                stm.setInt(5, (page-1)*size);
            }

            ResultSet searchedResults = stm.executeQuery();
            List<ItemDTO> items = new ArrayList<>();

            while (searchedResults.next()){
                items.add(new ItemDTO(
                        searchedResults.getString("user_email"),
                        searchedResults.getString("description"),
                        searchedResults.getString("status")
                ));
            }

            response.setContentType("application/json");

            ResultSet searchCount = stmCount.executeQuery();
            while (searchCount.next()){
                response.setHeader("X-Count", searchCount.getString(1));
            }

            Jsonb jsonb = JsonbBuilder.create();
            jsonb.toJson(items, response.getWriter());
        }catch (JsonbException e){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "JsonbException");
        }catch (Throwable t){
            t.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}