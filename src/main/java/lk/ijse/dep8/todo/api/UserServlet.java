package lk.ijse.dep8.todo.api;

import com.sun.org.apache.bcel.internal.generic.RET;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import lk.ijse.dep8.todo.dto.UserDTO;
import lk.ijse.dep8.todo.exception.ValidationException;
import sun.security.util.Password;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "UserServlet", value = { "/users/*"})
public class UserServlet extends HttpServlet {

    @Resource(name = "java:comp/env/jdbc/pool4todo")
    private volatile DataSource pool;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doSaveOrUpdate(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doSaveOrUpdate(request, response);
    }


    private void doSaveOrUpdate(HttpServletRequest request , HttpServletResponse response) throws IOException {

        /*Basically POST and PUT requests have json body*/
        if(request.getContentType() == null || !request.getContentType().toLowerCase().startsWith("application/json")){
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Required content type is not available");
            return;
        }

        String method = request.getMethod();
        String pathInfo = request.getPathInfo();

        if(method.equals("POST") && !(request.getServletPath().equalsIgnoreCase("/users") || request.getServletPath().equalsIgnoreCase("/users/"))){
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid url pattern");
            return;
        }else if(method.equals("PUT") && !(pathInfo != null && pathInfo.substring(1).matches("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"))){
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found in the DB");
            return;
        }

        try{
            Jsonb jsonb = JsonbBuilder.create();
            UserDTO user = jsonb.fromJson(request.getReader(), UserDTO.class);

            /*Validate the user details*/
            if(method.equals("POST") && (user.getEmail() == null || !user.getEmail().matches("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"))){
                throw new ValidationException("Invalid Email");
            }else if(user.getName() == null || !user.getName().matches("[A-Za-z ]+")){
                throw new ValidationException("Invalid Name");
            }else if(user.getPassword() == null || !user.getPassword().matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$")){
                /*
                Password must contain at least one digit [0-9].
                Password must contain at least one lowercase Latin character [a-z].
                Password must contain at least one uppercase Latin character [A-Z].
                Password must contain at least one special character like ! @ # & ( ).
                Password must contain a length of at least 8 characters and a maximum of 20 characters.
                 */
                throw new ValidationException("Invalid password");
            }

            if (method.equals("PUT")){
                user.setEmail(pathInfo.replaceAll("[/]", ""));
            }

            /*Create a connection with DB*/
            try(Connection connection = pool.getConnection()){
                PreparedStatement stm = connection.prepareStatement("SELECT * FROM User WHERE email=? ");
                stm.setString(1, user.getEmail());
                ResultSet results = stm.executeQuery();

                /*Save or Update logic*/
                if(results.next()){
                    if(method.equals("POST")){

                        response.sendError(HttpServletResponse.SC_CONFLICT, "This user already saved in the DB");
                    }else{

                        stm = connection.prepareStatement("UPDATE User SET name=?, password=? WHERE email=?");
                        stm.setString(1, user.getName());
                        stm.setString(2, user.getPassword());
                        stm.setString(3, user.getEmail());
                        int updatedResults = stm.executeUpdate();
                        if(updatedResults != 1){
                            throw new RuntimeException("Failed to Update user");
                        }
                        response.setStatus(HttpServletResponse.SC_CREATED);
                    }
                }else{

                    stm = connection.prepareStatement("INSERT INTO User (email, name, password) VALUES (?,?,?)");
                    stm.setString(1, user.getEmail());
                    stm.setString(2, user.getName());
                    stm.setString(3, user.getPassword());
                    int savedResults = stm.executeUpdate();
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
            response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Unable to delete all user yet");
            return;
        }else if (request.getPathInfo() != null && !request.getPathInfo().substring(1).matches("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")){
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "User not found");
            return;
        }

        String email = request.getPathInfo().replaceAll("[/]", "");

        try(Connection connection = pool.getConnection()){
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM User WHERE email=?");
            stm.setString(1, email);
            ResultSet findResults = stm.executeQuery();
            if(findResults.next()){
                stm = connection.prepareStatement("DELETE FROM User WHERE email=?");
                stm.setString(1, email);
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
        if(request.getPathInfo() != null && !request.getPathInfo().equals('/')){
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String query = request.getParameter("q");
        query = "%" + (query==null?"":query) + "%";

        try(Connection connection = pool.getConnection()){
            PreparedStatement stm = connection.prepareStatement("SELECT * FROM User WHERE email LIKE ? OR name LIKE ? OR password LIKE ?");
            stm.setString(1, query);
            stm.setString(2, query);
            stm.setString(3, query);
            ResultSet searchedResults = stm.executeQuery();

            List<UserDTO> users = new ArrayList<>();

            while (searchedResults.next()){
                users.add(new UserDTO(
                        searchedResults.getString("email"),
                        searchedResults.getString("name"),
                        searchedResults.getString("password")
                ));
            }

            response.setContentType("application/json");
            response.setHeader("X-Count", users.size()+"");

            Jsonb jsonb = JsonbBuilder.create();
            jsonb.toJson(users, response.getWriter());
        }catch (JsonbException e){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "JsonbException");
        }catch (Throwable t){
            t.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
