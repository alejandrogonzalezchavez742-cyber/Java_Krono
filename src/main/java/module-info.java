module com.mycompany.krono {
    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;      // AudioClip para alarma.wav

    // JDK
    requires java.sql;          // JDBC
    requires java.desktop;      // SystemTray, TrayIcon, Toolkit.beep()

    // Driver PostgreSQL (módulo automático)
    requires org.postgresql.jdbc;

    // FXMLLoader necesita reflexión sobre los controladores
    opens com.mycompany.krono.controller to javafx.fxml;
    // TableView/PropertyValueFactory leen los getters del modelo
    opens com.mycompany.krono.model to javafx.base;

    exports com.mycompany.krono;
}
