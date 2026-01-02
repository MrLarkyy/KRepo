package gg.aquatic.krepo.user

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UserInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(UserInitializer::class.java)

    override fun run(vararg args: String?) {
        if (userRepository.count() == 0L) {
            val initialPassword = UUID.randomUUID().toString().take(12)
            val admin = User(
                username = "admin",
                password = passwordEncoder.encode(initialPassword)!!,
                roles = setOf("ROLE_USER", "ROLE_ADMIN")
            )
            
            userRepository.save(admin)
            
            log.info("")
            log.info("###########################################################")
            log.info("#                                                         #")
            log.info("#   KRepo Initial Setup                                   #")
            log.info("#   Generated initial admin account:                      #")
            log.info("#   Username: admin                                       #")
            log.info("#   Password: $initialPassword                    #")
            log.info("#                                                         #")
            log.info("#   PLEASE CHANGE THIS PASSWORD IMMEDIATELY AFTER LOGIN!  #")
            log.info("#                                                         #")
            log.info("###########################################################")
            log.info("")
        }
    }
}
