package lk.ijse.dep8.todo.api;

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

@WebServlet(name = "UserServlet", value = {"/users/", "/users"})
public class UserServlet extends HttpServlet {

    @Resource(name = "java:comp/env/jdbc/pool4todo")
    private volatile DataSource pool;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        /*Add content type check*/
        if(request.getContentType() == null || !request.getContentType().toLowerCase().startsWith("application/json")){
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Wrong content type|");
            return;
        }

        /*Create validation to the json object*/
        try{
            Jsonb jsonb = JsonbBuilder.create();
            UserDTO user = jsonb.fromJson(request.getReader(), UserDTO.class);

            /*Validate the user details*/
            if(user.getEmail() == null || !user.getEmail().matches("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")){
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

            try(Connection connection = pool.getConnection()){

                PreparedStatement stm = null;
                stm = connection.prepareStatement("SELECT * FROM User WHERE email = ?");
                stm.setString(1, user.getEmail());
                ResultSet results = stm.executeQuery();
                if(results.next()){
                    /*There is a duplicate user*/
                    response.sendError(HttpServletResponse.SC_CONFLICT, "This user is already in the system");
                    return;
                }

                stm = connection.prepareStatement("INSERT INTO User (email, name, password) VALUES (?,?,?)");
                stm.setString(1, user.getEmail());
                stm.setString(2, user.getName());
                stm.setString(3, user.getPassword());

                int savedResult = stm.executeUpdate();
                if(savedResult != 1){
                    response.sendError(HttpServletResponse.SC_NO_CONTENT, "Failed to save user to the DB");
                    return;
                }
                response.sendError(HttpServletResponse.SC_CREATED, "Saved user to the DB");
            }



        }catch (ValidationException | JsonbException e){
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, (e instanceof JsonbBuilder)?"Invalid":e.getMessage());
        }catch (Throwable t){
            t.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }
}
