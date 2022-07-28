import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Data
public class Concept {
    private String code;
    private String label;
    private String preferredName;
    private boolean isRetired;
    @Getter(lazy = true)
    private final List<String> childCodes = new ArrayList<>();
    @Getter(lazy = true)
    private final List<String> subsetCodes = new ArrayList<>();
}
