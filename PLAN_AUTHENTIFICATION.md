# Plan d'implémentation — API d'authentification WebFlux (ÉTAT FINAL)

**Projet :** `com.service:student` (Spring Boot 4.1.2, Java 21, WebFlux + R2DBC + PostgreSQL + Flyway)  
**Cible :** API d'authentification stateless (JWT) 100 % réactive, endpoints `/api/v1/auth/*`  
**Statut :** ✅ Implémenté et testé avec Postman (signup, login, /me avec Bearer token)

---

## 1. Architecture retenue

### Séparation `User` (authentification) et `Customer` (métier)

On **ne modifie pas** `CustomerModel` ni la table `customers` pour y ajouter un mot de passe. On crée une entité `User` dédiée à l'authentification, liée à `Customer` par `customer_id` (clé étrangère optionnelle).

| Raison | Explication |
|--------|-------------|
| Séparation des préoccupations | `Customer` = données métier (nom, adresse, taxId). `User` = identité de connexion (email, mot de passe hashé, token). |
| Sécurité | Le hash de mot de passe n'est jamais mélangé aux données clients exposés par `/api/v1/customers`. |
| Maintenabilité | Une modification de la table `customers` n'impacte pas le flux d'authentification. |
| Spring Security | Le `SecurityContext` WebFlux manipule un principal léger (`User` ou `Jwt`), pas un `CustomerModel` complet. |

### Schéma relationnel

```
USERS                      CUSTOMERS
--------                   ---------
user_id   UUID PK          customer_id  UUID PK
email     VARCHAR UK       company_name VARCHAR
password  VARCHAR          company_email VARCHAR
username  VARCHAR          tax_id         VARCHAR
roles     VARCHAR          billing_* / shipping_*
active    BOOLEAN
locked    BOOLEAN
enabled   BOOLEAN
created_at TIMESTAMP
last_login_at TIMESTAMP
customer_id UUID FK -->----> CUSTOMERS(customer_id)
```

> `customer_id` est optionnel : un `User` peut exister sans profil `Customer` associé.

---

## 2. Dépendances (`pom.xml`)

Déjà présentes dans le projet :

```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
</dependency>

<!-- OAuth2 Resource Server (pour valider les JWT dans Spring Security WebFlux) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<!-- Spring Security (déjà présent) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

---

## 3. Configuration (`application.yaml`)

```yaml
jwt:
  secret: JRBUmlElEifq+MzlYMej72E35U5ydrf6wXujs4lx/xg=   # Base64, >= 256 bits
  access-token-expiration: 3600000    # 1 heure
  refresh-token-expiration: 604800000 # 7 jours
```

> ⚠️ **Règle critique** : cette clé secrète doit être **identique** dans `TokenManager` et `SecurityConfig`. Si elles diffèrent, la validation du JWT échoue (401).

---

## 4. Migration Flyway (`V3__create_users_table.sql`)

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE USERS (
    user_id       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password      VARCHAR(255) NOT NULL,
    username      VARCHAR(255),
    roles         VARCHAR(255),
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    locked        BOOLEAN NOT NULL DEFAULT FALSE,
    enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMP,
    customer_id   UUID REFERENCES CUSTOMERS(customer_id)
);

CREATE INDEX IDX_USERS_EMAIL ON USERS(email);
CREATE INDEX IDX_USERS_CUSTOMER_ID ON USERS(customer_id);
```

---

## 5. Domain

### `domain/User.java` (déjà implémenté)

```java
@Table("users")
public record User(
    UUID userId,
    String email,
    String password,
    String username,
    Set<String> roles,
    boolean active,
    boolean locked,
    boolean enabled,
    Instant createdAt,
    Instant lastLoginAt
) {}
```

> `password` contient le **hash BCrypt**, jamais le mot de passe en clair.

---

## 6. Repository

### `repository/UserRepository.java` (déjà implémenté)

```java
public interface UserRepository extends ReactiveCrudRepository<User, UUID> {
    Mono<User> findByEmail(String email);
    Mono<User> findByUsername(String username);
    Mono<Boolean> existsByEmail(String email);
    Mono<Boolean> existsByUsername(String username);
}
```

---

## 7. DTOs

### `dto/SignupRequest.java`
```java
public record SignupRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password,
    @NotBlank String username
) {}
```

### `dto/LoginRequest.java`
```java
public record LoginRequest(String email, String password) {}
```

### `dto/AuthResponse.java`
```java
public record AuthResponse(
    String userId,
    String email,
    String token,
    String tokenType,   // "Bearer"
    long expiresIn
) {}
```

### `dto/UserDto.java` (pour `/me`, sans exposer le password hash)
```java
public record UserDto(String userId, String email, Set<String> roles) {
    public static UserDto from(User user) {
        return new UserDto(user.userId().toString(), user.email(), user.roles());
    }
}
```

---

## 8. Exceptions métier

- `exception/UserExistsException.java` → HTTP 409 (email déjà utilisé)
- `exception/InvalidCredentialsException.java` → HTTP 401 (email/mot de passe invalide)
- `exception/InvalidTokenException.java` → token invalide/expiré

---

## 9. TokenManager (`security/TokenManager.java`) ✅ Implémenté

Classe utilitaire synchrone (pas de `Mono`, pas de Spring Web) pour générer et valider les JWT.

**Points clés :**
- Injection de `jwt.secret` via `@Value` dans le constructeur
- Création de la clé HMAC : `Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))`
- Génération : `Jwts.builder().subject(email).claim("userId", ...).claim("roles", ...).signWith(HS256, key).compact()`
- Validation : `parseSignedClaims` + vérification d'expiration
- Retourne `Optional<Claims>` pour `extractClaims` (jamais de `null` brutal)

**Méthodes :**
| Méthode | Rôle |
|---------|------|
| `generateToken(userId, email, roles)` | Crée un JWT signé HS256 |
| `extractClaims(token)` | Retourne `Optional<Claims>` (vide si invalide) |
| `extractEmail(token)` | Lit le claim `sub` |
| `extractUserId(token)` | Lit le claim `userId` |
| `extractExpiration(token)` | Lit la date d'expiration |
| `isExpiredToken(token)` | `true` si expiré ou illisible |
| `validateToken(token)` | `true` si signature valide et non expiré |
| `generateRefreshToken(email)` | Crée un refresh token (claim `type=refresh`) |
| `getAuthorities(token)` | Transforme les rôles en `List<SimpleGrantedAuthority>` |
| `isRefreshToken(token)` | Vérifie le claim `type` |

---

## 9. Service d'authentification (`service/AuthService.java` + `AuthServiceImpl.java`) ✅ Implémenté

### Interface `AuthService`
```java
Mono<AuthResponse> signup(SignupRequest signupRequest);
Mono<AuthResponse> login(LoginRequest loginRequest);
```

### `AuthServiceImpl` (règles réactives : aucun `.block()`)

**signup :**
1. `existsByEmail(email)` → si vrai, `Mono.error(UserExistsException)`
2. `BCrypt.hashpw(password, BCrypt.gensalt())`
3. `userRepository.save(user)` (UUID généré en Java avant sauvegarde pour éviter le NPE sur `userId` null avec R2DBC)
4. `tokenManager.generateToken(userId, email, roles)`
5. Retour `AuthResponse(userId, email, token, "Bearer", exp)`

**login :**
1. `findByEmail(email)` → si vide, `Mono.error(InvalidCredentialsException)`
2. `BCrypt.checkpw(password, user.password())` → si faux, `Mono.error(InvalidCredentialsException)`
3. `tokenManager.generateToken(...)`
4. Retour `AuthResponse`

> Message générique "Invalid email or password" dans les deux cas (évite l'énumération d'utilisateurs).

---

## 10. Contrôleur (`controller/AuthController.java`) ✅ Implémenté

| Méthode | Path | Corps | Sécurité | Rôle |
|---------|------|-------|----------|------|
| `POST` | `/api/v1/auth/signup` | `SignupRequest` | Public | Crée un compte + retourne le JWT |
| `POST` | `/api/v1/auth/login` | `LoginRequest` | Public | Vérifie les credentials + retourne le JWT |
| `GET` | `/api/v1/auth/me` | — | Protégé | Retourne les infos de l'utilisateur connecté |

### `/me` avec `Authentication`
```java
@GetMapping("/auth/me")
public Mono<ResponseEntity<?>> getCurrentUser(Authentication authentication) {
    Object principal = authentication.getPrincipal();

    if (principal instanceof Jwt jwt) {
        String userId = jwt.getClaimAsString("userId");
        String email = jwt.getClaimAsString("sub");
        List<String> rolesFromClaim = jwt.getClaimAsStringList("roles");
        Set<String> roles = rolesFromClaim == null
            ? new HashSet<>()
            : new HashSet<>(rolesFromClaim);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userId);
        userInfo.put("email", email);
        userInfo.put("roles", roles);

        return Mono.just(ResponseEntity.ok(userInfo));
    }

    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
}
```

> ⚠️ `Map.of()` ne accepte **pas** les valeurs `null`. Utiliser `HashMap` ou vérifier les null avant.

---

## 11. Sécurité Spring WebFlux (`security/SecurityConfig.java`) ✅ Implémenté

### `SecurityWebFilterChain`
```java
@Bean
public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    return http
        .csrf(csrf -> csrf.disable())
        .exceptionHandling(exceptionHandling -> exceptionHandling
            .authenticationEntryPoint(new HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED))
            .accessDeniedHandler(new HttpStatusServerAccessDeniedHandler(HttpStatus.FORBIDDEN))
        )
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers(HttpMethod.POST, "/api/v1/auth/signup", "/api/v1/auth/login").permitAll()
            .pathMatchers("/actuator/**").permitAll()
            .anyExchange().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
        )
        .build();
}
```

### `ReactiveJwtDecoder`
```java
@Bean
public ReactiveJwtDecoder reactiveJwtDecoder() {
    SecretKey secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
}
```

### `Converter<Jwt, Mono<AbstractAuthenticationToken>>` (JWT → Authentication)
Dans WebFlux, le converter doit retourner un `Mono` (pas un `AbstractAuthenticationToken` brut) :

```java
@Bean
public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
    JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
    delegate.setJwtGrantedAuthoritiesConverter(jwt -> {
        List<String> rolesFromClaim = jwt.getClaimAsStringList("roles");
        Set<String> roles = rolesFromClaim == null
            ? new HashSet<>()
            : new HashSet<>(rolesFromClaim);
        return roles.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toSet());
    });
    return jwt -> Mono.just(delegate.convert(jwt));
}
```

> ⚠️ Erreur classique : `Objects.requireNonNull(jwt.getClaimAsStringList("roles"))` lance un `NPE` si le claim `"roles"` est absent. Toujours gérer le `null`.

---

## 12. CORS (`config/WebFluxCorsConfiguration.java`) ✅ Mis à jour

Ajouter l'en-tête `Authorization` pour que le token JWT passe en preflight :

```java
corsConfiguration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-USER_ID"));
```

---

## 13. Ce qui N'EST PAS nécessaire (ni implémenté, ni requis)

| Composant | Pourquoi absent |
|-----------|-----------------|
| `JwtAuthenticationFilter implements WebFilter` | Spring Security installe déjà un `BearerTokenAuthenticationFilter` interne via `.oauth2ResourceServer().jwt()`. Un filtre custom n'est utile que pour une blacklist de tokens, un audit, ou un enrichissement depuis la BDD. |
| `ReactiveUserDetailsService` | L'authentification est stateless : le JWT contient déjà email, userId, rôles. Aucun besoin d'aller en base à chaque requête pour charger un `UserDetails`. Utile uniquement si l'on doit vérifier `enabled`/`locked` en temps réel depuis la base. |

---

## 14. Problèmes rencontrés et solutions (pour référence future)

| Problème | Cause | Solution |
|----------|-------|----------|
| `WeakKeyException: 48 bits` | Clé secrète trop courte (< 256 bits) | Utiliser une clé Base64 d'au moins 32 octets (`openssl rand -base64 32`) |
| `NPE` sur `Map.of(userId, email, roles)` | `Map.of` n'accepte pas `null` | Utiliser `HashMap` ou vérifier les null avant |
| `NPE` sur `Objects.requireNonNull(jwt.getClaimAsStringList("roles"))` | Le claim `"roles"` est absent du JWT | Gérer le `null` avec `== null ? new<|constrain|>` |
| `NPE` sur `savedUser.userId().toString()` | R2DBC ne remplit pas automatiquement l'ID généré | Générer l'UUID en Java avant `save()` : `UUID.randomUUID()` |
| Clé secrète incohérente | `TokenManager` utilisait `getBytes(UTF_8)`, `SecurityConfig` utilisait `Decoders.BASE64.decode` | Unifier sur `Decoders.BASE64.decode` partout |

---

## 15. Checklist de validation finale

```
☐ 1. Dépendances jjwt + oauth2-resource-server dans pom.xml
☐ 2. application.yaml contient jwt.secret, access-token-expiration, refresh-token-expiration
☐ 3. Migration V3__create_users_table.sql appliquée (table USERS visible dans PostgreSQL)
☐ 4. domain/User avec 11 champs (incluant password hash BCrypt)
☐ 5. UserRepository avec findByEmail, existsByEmail (retour Mono<User>)
☐ 6. DTOs : SignupRequest, LoginRequest, AuthResponse, UserDto
☐ 7. Exceptions : UserExistsException, InvalidCredentialsException
☐ 8. TokenManager : génération, validation, refresh, autorités
☐ 9. AuthServiceImpl : signup + login réactifs, aucun .block(), BCrypt
☐ 10. AuthController : POST /signup, POST /login, GET /me (avec Authentication + Jwt)
☐ 11. SecurityConfig : SecurityWebFilterChain, ReactiveJwtDecoder, Converter<Jwt, Mono<...>>
☐ 12. WebFluxCorsConfiguration : header Authorization autorisé
☐ 13. POST /signup → 201 + token
☐ 14. POST /login → 200 + token
☐ 15. GET /me sans token → 401
☐ 16. GET /me avec Bearer token → 200 + infos utilisateur
☐ 17. ./mvnw compile : pas d'erreur
```

---

## 16. Technique pédagogique (pour un développeur junior)

Voir la section originale du plan (méthode "Découvrir → Tester → Implémenter → Intégrer → Reproduire"). Les étapes clés :

1. Comprendre que `is...` / `has...` posent une question oui/non
2. Comprendre que `Mono` ne se teste pas avec `== null` (utiliser `switchIfEmpty` / `flatMap`)
3. Comprendre que `Map.of` rejette les `null`
4. Comprendre que la clé secrète doit être identique entre `TokenManager` et `SecurityConfig`
5. Comprendre qu'aucun filtre WebFilter ni UserDetailsService n'est nécessaire pour du JWT stateless

---

## 17. Points de vigilance

- **Zéro `.block()`** dans le flux réactif.
- **Mot de passe hashé** : jamais retourné dans un DTO (utiliser `UserDto`, pas `User`).
- **Clé secrète** : jamais en dur dans le code, toujours via `@Value("${jwt.secret}")`.
- **CORS** : ajouter `Authorization` aux en-têtes autorisés.
- **Token expiré / invalide** : `validateToken` doit retourner `false`, jamais lever d'exception non attrapée.
- **Claim "roles"** : peut être absent du JWT ; toujours gérer le `null` avant `Map.of` ou `HashSet`.

---

Le plan est désormais à jour avec l'implémentation réelle du repo. L'API d'authentification WebFlux est complète et fonctionnelle.