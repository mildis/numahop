package fr.progilone.pgcn.service.ocrlangconfiguration.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import fr.progilone.pgcn.domain.dto.ocrlangconfiguration.SimpleOcrLangConfigDTO;
import fr.progilone.pgcn.domain.ocrlangconfiguration.OcrLangConfiguration;
import fr.progilone.pgcn.service.library.mapper.LibraryMapper;


@Mapper(uses = {OcrLanguageMapper.class, LibraryMapper.class})
public interface OcrLangConfigurationMapper {
    
    OcrLangConfigurationMapper INSTANCE = Mappers.getMapper(OcrLangConfigurationMapper.class);
    
    SimpleOcrLangConfigDTO ocrLangConfigToSimpleDto(OcrLangConfiguration conf);
    
}
