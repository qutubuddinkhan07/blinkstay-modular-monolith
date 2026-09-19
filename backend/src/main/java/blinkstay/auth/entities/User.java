package blinkstay.auth.entities;

import blinkstay.auth.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "username cannot be empty")
    @Column(nullable = false)
    private String username;

    @NotBlank(message = "email cannot be empty")
    @Column(nullable = false,unique = true)
    private String email;

    @NotBlank(message = "password cannot be empty")
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)    // stores "USER"/"ADMIN" in DB, not 0/1
    private UserRole role;

    @Builder.Default
    private Boolean isActive=true;

    // Stores Cloudinary image URL
    private String profileImgUrl;

    // Store Cluodinary public_id for deletion/updates
    private String imagePublicId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /*-
    * instead of @CreationTimeStamp can use this
    * but need to write write @Builder.Default for the builder
    * but @CreationTimeStamp handles both
    * @PrePersist
      protected void onCreate(){
          this.createdAt = LocalDateTime.now();
      }
     */
}
