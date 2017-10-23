package eu.einfracentral.registry.service;

import eu.einfracentral.domain.aai.User;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.ParserService;
import eu.openminted.registry.core.service.SearchService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by pgl on 07/08/17.
 */
@org.springframework.stereotype.Service("userService")
@Configurable
@PropertySource({"classpath:eu/einfracentral/domain/application.properties"})
public class UserServiceImpl<T> extends BaseGenericResourceCRUDServiceImpl<User> implements UserService {

    @Autowired
    private MailService mailService;

    @Value("${mail.activate.subject}")
    private String activateSubject;

    @Value("${mail.reset.subject}")
    private String resetSubject;

    @Value("${mail.activate.text}")
    private String activateText;

    @Value("${mail.reset.text}")
    private String resetText;

    public UserServiceImpl() {
        super(User.class);
    }

    @Override
    public String getResourceType() {
        return "einfrauser";
    }

    @Override
    public User activate(String id) {
        User ret = unsafeGet(id);
        if (ret.getJoinDate() == null) {
            ret.setJoinDate(new Date().toString());
            update(ret);
            //Rollback error exists up to 1.3.1-20170804.135357-7, other errors appear aftewards
        } else {
            throw new RESTException("User already activated", HttpStatus.CONFLICT);
        }
        return strip(ret);
    }

    @Override
    public User reset(User user) {
        User ret = null;
        if (user.getResetToken().equals(unsafeGet(user.getId()).getResetToken())) {
            ret = hashUser(user);
            update(ret);
        }
        return strip(ret);
    }

    @Override
    public User forgot(String email) {
        User ret = getUserByEmail(email);
        if (ret != null) {
            ret.setResetToken("Generate THIS!");
            update(ret);
            mailService.sendMail(ret.getEmail(), resetSubject, resetText + ret.getId() + "/" + ret.getResetToken());

        }
        return strip(ret);
    }

//    @Override
//    public Browsing getAll(FacetFilter facetFilter) {
//        return new Browsing(0, 0, 0, new ArrayList<Order>(), new ArrayList<Facet>());
//    }

    @Override
    public User register(User user) {
        User ret = null;
        if (getUserByEmail(user.getEmail()) == null) {
            user.setId(UUID.randomUUID().toString());
            ret = hashUser(user);
            add(ret, ParserService.ParserServiceTypes.JSON);
            mailService.sendMail(user.getEmail(), activateSubject, activateText + user.getId());
        }
        return strip(ret); //Not using get(ret.getId()) here, because this line runs before the db is updated
    }

    @Value("${sec.user.iterations:1000}")
    private int currentServerIterationCount;

    private User hashUser(User user) {
        final Random r = new SecureRandom();
        byte[] salt = new byte[8];
        r.nextBytes(salt);
        user.setSalt(salt);
        user.setPassword(new String(hashPass(user.getPassword().toCharArray(), user.getSalt(), currentServerIterationCount)));
        return user;
    }

    private char[] hashPass(char[] pass, byte[] salt, int iterations) {
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            PBEKeySpec spec = new PBEKeySpec(pass, salt, iterations, 256);
            SecretKey key = skf.generateSecret(spec);
            return new String(Base64.getEncoder().encode(key.getEncoded())).toCharArray();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new Error(ex);
        }
    }

    @Override
    public boolean authenticate(User credentials) {
        User actual = unsafeGet(getUserByEmail(credentials.getEmail()).getId());
        return Arrays.equals(hashPass(credentials.getPassword().toCharArray(), actual.getSalt(), actual.getIterationCount()), actual.getPassword().toCharArray());
    }

    @Override
    public User getUserByEmail(String email) {
        User ret = null;
        try {
            Resource foundResource = searchService.searchId(getResourceType(), new SearchService.KeyValue("email", email));
            if (foundResource != null) {
                User foundUser = parserPool.serialize(foundResource, typeParameterClass).get();
                if (foundUser != null) {
                    ret = strip(foundUser);
                }
            }
        } catch (UnknownHostException | InterruptedException | ExecutionException e) {
            throw new RESTException(e, HttpStatus.NOT_FOUND);
        }
        return ret;
    }

    @Value("${jwt.secret:}")
    private String secret;

    @Override
    public String getToken(User credentials) {
        if (secret.length() == 0)
            throw new RESTException("jwt.secret has not been set", HttpStatus.INTERNAL_SERVER_ERROR);
        Date now = new Date();
        if (authenticate(credentials)) {
            return Jwts.builder().
                    setSubject(credentials.getEmail())
                    .claim("roles", "user")
                    .setIssuedAt(now)
                    .setExpiration(new Date(now.getTime() + 86400000))
                    .signWith(SignatureAlgorithm.HS256, secret)
                    .compact();
        } else {
            throw new RESTException("Passwords do not match.", HttpStatus.FORBIDDEN);
        }
    }

    @Override
    public User get(String id) {
        return strip(unsafeGet(id));
    }

    private User strip(User user) {
        user.setPassword("");
        user.setResetToken("");
        user.setSalt(new byte[0]);
        user.setIterationCount(0);
        return user;
    }

    private User unsafeGet(String id) {
        return super.get(id);
    }

}
