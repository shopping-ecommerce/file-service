package iuh.fit.fe.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "file_mgmt")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FileMgmt {
    @MongoId
    String id;
    String contentType;
    long size;
    String path;
    String md5Checksum;
    String ownerId;
}
