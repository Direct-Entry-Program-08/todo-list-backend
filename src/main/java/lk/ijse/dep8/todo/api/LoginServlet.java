package lk.ijse.dep8.todo.api;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import lk.ijse.dep8.todo.dto.LoginDTO;
import lk.ijse.dep8.todo.exception.ValidationException;

import javax.annotation.Resource;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import javax.sql.DataSource;
import java.io.IOException;

@WebServlet(name = "LoginServlet", value = {"/logins", "/logins/"})
public class LoginServlet extends HttpServlet {

    @Resource(name = "java:comp/env/jdbc/pool4todo")
    private volatile DataSource pool;


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        /*First check the content type*/
        if(request.getContentType() == null || !request.getContentType().toLowerCase().startsWith("application/json")){
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "Invalid content type");
            return;
        }else if(request.getMethod().equals("POST") && !(request.getServletPath().equalsIgnoreCase("/logins") || request.getServletPath().equalsIgnoreCase("/logins/"))){
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid URL pattern");
        }

        try{
            Jsonb jsonb = JsonbBuilder.create();
            LoginDTO login = jsonb.fromJson(request.getReader(), LoginDTO.class);

            /*First validation of the data*/
            if(login.getEmail() == null || !login.getEmail().matches("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])")){
                throw new ValidationException("Invalid Email");
            }else if(login.getPassword() == null || !login.getPassword().matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$")){
                throw new ValidationException("Invalid Password");
            }

        }catch (JsonbException | ValidationException e){
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, (e instanceof JsonbException)?"Invalid json":e.getMessage());
        }catch (Throwable t){
            t.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }
}
