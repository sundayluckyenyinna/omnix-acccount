/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.account;

import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;

import java.io.*;
import java.lang.System.Logger;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 *
 * @author bokon
 */
@Configuration
//@ComponentScan
@EntityScan("com.accionmfb.omnix.account.model")
@PropertySource("classpath:application.yml")
@EnableTransactionManagement
//@EnableWebMvc
@EnableEncryptableProperties
@EnableAsync
public class AppConfig extends WebMvcConfigurerAdapter {

    @Autowired
    Environment env;
    protected Logger logger;

    @Bean()
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(env.getProperty("spring.datasource.driverClassName"));
        dataSource.setUrl(env.getProperty("spring.datasource.url"));
        dataSource.setUsername(env.getProperty("spring.datasource.username"));
        dataSource.setPassword(env.getProperty("spring.datasource.password"));
        return dataSource;
    }

    @Bean()
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource());
        em.setPackagesToScan(new String[]{"com.accionmfb.omnix.account.model"});
        em.setPersistenceUnitName("corePersistenceUnit");
        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        final Properties properties = new Properties();
        properties.setProperty("hibernate.hbm2ddl.auto", env.getProperty("spring.jpa.hibernate.ddl-auto"));
        properties.setProperty("hibernate.dialect", env.getProperty("spring.jpa.hibernate.dialect"));
        em.setJpaProperties(properties);

        return em;
    }

    @Bean()
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);

        return transactionManager;
    }

    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        YamlPropertiesFactoryBean bean = new YamlPropertiesFactoryBean();
        bean.setResources(new ClassPathResource("messages.yml"));
        messageSource.setCommonMessages(bean.getObject());
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(0);
        return messageSource;
    }

    @Bean(name = "validator")
    public LocalValidatorFactoryBean validator() {
        LocalValidatorFactoryBean bean = new LocalValidatorFactoryBean();
        bean.setValidationMessageSource(messageSource());
        return bean;
    }

    @Bean(name = "jasyptStringEncryptor")
    public StringEncryptor getPasswordEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword("H*-lLo5,e:2.VN"); // encryptor's private key
        config.setAlgorithm("PBEWithMD5AndDES");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setStringOutputType("base64");

        encryptor.setConfig(config);
        return encryptor;
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(1000);
        executor.setCorePoolSize(1000);
        executor.setThreadNamePrefix("default_task_executor_thread");
        executor.initialize();
        return executor;
    }

    @Bean
    public CommandLineRunner runner() throws IOException {
        return ((args) -> {
            Document jsoupDocument = Jsoup.connect("http://localhost:1000/account-statement.html").ignoreHttpErrors(true).get();
            System.out.println("Document html: " + jsoupDocument.outerHtml());

            String path = "test-account-statement.pdf";
            File pdf = new File("test-account-statement.pdf");
            FileOutputStream stream = new FileOutputStream(pdf);

            PdfWriter writer = new PdfWriter(path);
            PdfDocument pdfDocument = new PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdfDocument);

            InputStream ins = new ByteArrayInputStream(jsoupDocument.outerHtml().getBytes(StandardCharsets.UTF_8));
            HtmlConverter.convertToPdf(jsoupDocument.outerHtml(), stream);
        });
    }
    
}
