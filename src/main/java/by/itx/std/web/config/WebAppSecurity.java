package by.itx.std.web.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

@Configuration
@EnableWebSecurity
public class WebAppSecurity extends WebSecurityConfigurerAdapter {

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/static/**");
    }

//    private static final Integer STRENGTH = 20;
//
//    @Autowired
//    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
////        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder(){
//        return new BCryptPasswordEncoder(STRENGTH);
//    }
//
//    @Override
//    protected void configure(HttpSecurity http) throws Exception {
//        http
//                .authorizeRequests()
//                .antMatchers("/admin/**").access("hasRole('ROLE_ADMIN')")
//                .and()
//                .formLogin()
//                .successHandler(savedRequestAwareAuthenticationSuccessHandler())
//                .loginPage("/login")
//                .failureUrl("/login?error")
//                .loginProcessingUrl("/auth/login_check")
//                .usernameParameter("username")
//                .passwordParameter("password")
//                .and()
//                .logout().logoutSuccessUrl("/login?logout")
//                .and()
//                .csrf()
//                .and()
//                .rememberMe()
//                    .tokenRepository(persistentTokenRepository())
//                .and().exceptionHandling().accessDeniedPage("/403");
//    }
//
//    @Bean
//    public PersistentTokenRepository persistentTokenRepository() {
//        JdbcTokenRepositoryImpl db = new JdbcTokenRepositoryImpl();
////        db.setDataSource(dataSource);
//        return db;
//    }
//
//    @Bean
//    public SavedRequestAwareAuthenticationSuccessHandler
//    savedRequestAwareAuthenticationSuccessHandler() {
//
//        SavedRequestAwareAuthenticationSuccessHandler auth
//                = new SavedRequestAwareAuthenticationSuccessHandler();
//        auth.setTargetUrlParameter("targetUrl");
//        return auth;
//    }

}
