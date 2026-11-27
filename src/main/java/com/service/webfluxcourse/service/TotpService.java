package com.service.webfluxcourse.service;

import com.service.webfluxcourse.domain.User;
import com.service.webfluxcourse.exception.UserNotFoundException;
import com.service.webfluxcourse.repository.UserRepository;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.UUID;

@Service
public class TotpService {

    private final UserRepository userRepository;

    private final SecretGenerator secretGenerator;
    private final QrGenerator qrGenerator;

    private final CodeVerifier verifier;

    public TotpService(UserRepository userRepository,
                       SecretGenerator secretGenerator,
                       QrGenerator qrGenerator,
                       CodeVerifier verifier) {
        this.userRepository = userRepository;
        this.secretGenerator = secretGenerator;
        this.qrGenerator = qrGenerator;
        this.verifier = verifier;
    }

    /**
     * Génère un secret TOTP aléatoire (Base32).
     *
     * @return
     */
    public String generateSecret() {
        return secretGenerator.generate();
    }

    /**
     * Vérifie qu'un code TOTP à 6 chiffres correspond au secret stocké.
     */
    public boolean verifyCode(String secret, String code) {

        if (secret == null || code == null) {
            return false;
        }

        return verifier.isValidCode(secret, code);
    }

    public String generateQrCodeUri(String secret, String email, String issuer) {
          try {
              QrData data = new QrData.Builder()
                      .label(email)
                      .secret(secret)
                      .issuer(issuer)
                      .algorithm(HashingAlgorithm.SHA1)
                      .digits(6)
                      .period(30)
                      .build();

              byte[] qrCodeImage= qrGenerator.generate(data);
              String mimeType= qrGenerator.getImageMimeType();

              // On construit la date URI: data:image/png; base64
              String base64= Base64.getEncoder().encodeToString(qrCodeImage);
              return "data:" + mimeType + ";base64," + base64;
          }catch (QrGenerationException e) {
              throw new RuntimeException(" Erreur lors de la génération du QR code" ,e);
          }

    }

    /**
     * Sauvegarde le secret TOTP pour un utilisateur (totpEnabled reste à false
     * tant que le premier code n'a pas été validé).
     */
    public Mono<User> saveTotpSecret(UUID userId, String secret)  {

        return userRepository.findById(userId).switchIfEmpty(Mono.error(new UserNotFoundException("Utilisateur n'existe pas")))
                .flatMap(user -> {

                    User updated = new User(user.userId(), user.email(), user.password(), user.username(), user.roles(), user.active(), user.locked(),
                            user.enabled(), user.createdAt(), user.lastLoginAt(), secret, user.totpEnabled());
                    return userRepository.save(updated);
                });
    }

    /**
     * Active le TOTP pour un utilisateur après validation du premier code.
     */
    public Mono<User> enableTotp(UUID userId) {
        return userRepository.findById(userId).switchIfEmpty(Mono.error(new UserNotFoundException("Utilisateur n'existe pas"))).flatMap(
                user -> {

                    User updatedUser = new User(
                            user.userId(),
                            user.email(),
                            user.password(),
                            user.username(),
                            user.roles(),
                            user.active(),
                            user.locked(),
                            user.enabled(),
                            user.createdAt(),
                            user.lastLoginAt(),
                            user.totpSecret(),
                            true // totEnabled= true,

                    );

                    return userRepository.save(updatedUser);
                });
    }


    /**
     * Désactive le TOTP (ex: réinitialisation).
     */
    public Mono<User> disableTotp(UUID userId) {
        return userRepository.findById(userId).switchIfEmpty(Mono.error(new UserNotFoundException("Utilisateur n'existe pas")))
                .flatMap(
                        user -> {
                            User updatedUser = new User(user.userId(),
                                    user.email(),
                                    user.password(),
                                    user.username(),
                                    user.roles(),
                                    user.active(),
                                    user.locked(),
                                    user.enabled(),
                                    user.createdAt(),
                                    user.lastLoginAt(),
                                    null,  // totpSecret supprimé //
                                    false  // totpEnabled = false
                            );

                            return userRepository.save(updatedUser);
                        }
                );
    }
}
