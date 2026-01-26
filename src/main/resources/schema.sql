CREATE TABLE users(
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      email VARCHAR(20) NOT NULL UNIQUE,
                      password VARCHAR(100) NOT NULL,
                      name VARCHAR(20),
                      market_access_token TEXT,
                      birth_date DATE,
                      oauth_token TEXT,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE trade(
                      id BIGINT PRIMARY KEY AUTO_INCREMENT,
                      firm ENUM('LS') NOT NULL ,
                      stock_code VARCHAR(10) NOT NULL ,
                      user_set_price INT NOT NULL,
                      quantity INT NOT NULL,
                      executed_price INT ,
                      trade_status ENUM('ACTIVE','CANCELED', 'EXPIRED', 'EXECUTED_BUY','EXECUTED_LOSSCUT') NOT NULL,
                      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                      user_id BIGINT,
                      FOREIGN KEY (user_id) REFERENCES users(id)
);