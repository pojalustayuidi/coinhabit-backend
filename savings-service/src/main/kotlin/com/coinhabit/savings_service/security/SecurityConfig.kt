
package com.coinhabit.savings_service.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtService: JwtService
    ): SecurityFilterChain {

        return http
            .csrf { csrf ->
                csrf.disable()
            }
            .cors { cors ->
                cors.disable()
            }
            .sessionManagement { session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            }
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers("/api/savings/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll()
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(
                    HttpStatusEntryPoint(
                        HttpStatus.UNAUTHORIZED
                    )
                )
            }
            .httpBasic { basic ->
                basic.disable()
            }
            .formLogin { form ->
                form.disable()
            }
            .addFilterBefore(
                JwtAuthenticationFilter(jwtService),
                UsernamePasswordAuthenticationFilter::class.java
            )
            .build()
    }
}
