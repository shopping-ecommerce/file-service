package iuh.fit.fe.mapper;

import iuh.fit.fe.dto.FileInfo;
import iuh.fit.fe.entity.FileMgmt;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FileMgmtMapper {
    @Mapping(target = "id",source = "name")
    FileMgmt toFileMgmt(FileInfo fileInfo);
}
