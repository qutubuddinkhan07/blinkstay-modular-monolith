package blinkstay.seed.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class SeedReviewData {

    private List<Guest> guests;

    // key = rating ("5", "4", "3"), value = comments for that rating
    private Map<String, List<String>> comments;

    // {guest} is replaced with the guest's first name
    private List<String> replies;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    public static class Guest {
        private String username;
        private String email;
    }
}