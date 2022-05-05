USE dep8_todo;

CREATE TABLE User(
                     email VARCHAR(30) NOT NULL,
                     name VARCHAR(30) NOT NULL ,
                     password VARCHAR(30) NOT NULL ,
                     CONSTRAINT PRIMARY KEY (email)
);

CREATE TABLE Item(
                     id INT AUTO_INCREMENT,
                     description VARCHAR(50) NOT NULL ,
                     status ENUM ('DONE', 'NOT-DONE'),
                     user_mail VARCHAR(6) NOT NULL,
                     CONSTRAINT PRIMARY KEY (id),
                     CONSTRAINT FOREIGN KEY(user_mail) REFERENCES User(email) on Delete Cascade on Update Cascade
);