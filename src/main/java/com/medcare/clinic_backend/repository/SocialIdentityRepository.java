package com.medcare.clinic_backend.repository;

import com.medcare.clinic_backend.entity.SocialIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialIdentityRepository extends JpaRepository<SocialIdentity, Integer> {

    Optional<SocialIdentity> findByProviderAndProviderUserId(String provider, String providerUserId);

    Optional<SocialIdentity> findByProviderAndEmailIgnoreCase(String provider, String email);
}
