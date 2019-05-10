package fr.progilone.pgcn.domain.dto.library;

import java.util.List;

import fr.progilone.pgcn.domain.dto.AbstractDTO;

/**
 * DTO représentant un paramètre de bibliothèque
 * 
 * @author jbrunet
 * Créé le 24 févr. 2017
 */
public class LibraryParameterValuedDTO extends AbstractDTO {

    private String identifier;
    private String type;
    private SimpleLibraryDTO library;
    private List<LibraryParameterValueCinesDTO> values;
    
    public LibraryParameterValuedDTO() {}

    public String getIdentifier() {
        return identifier;
    }

	public final void setIdentifier(final String identifier) {
		this.identifier = identifier;
	}

    public final String getType() {
        return type;
    }

    public final void setType(final String type) {
        this.type = type;
    }

    public final SimpleLibraryDTO getLibrary() {
        return library;
    }

    public final void setLibrary(final SimpleLibraryDTO library) {
        this.library = library;
    }

    public List<LibraryParameterValueCinesDTO> getValues() {
        return values;
    }

    public void setValues(final List<LibraryParameterValueCinesDTO> values) {
        this.values = values;
    }

  
}
