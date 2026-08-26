package com.iba;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class IbaSistemaApplication {

    public static void main(String[] args) {

        try {
            // Tenta carregar .env (apenas para desenvolvimento local)
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();

            // Só define as propriedades se o .env existir
            if (dotenv != null) {
                System.setProperty("DB_USER", dotenv.get("DB_USER"));
                System.setProperty("DB_PASSWORD", dotenv.get("DB_PASSWORD"));
                System.setProperty("DB_URL", dotenv.get("DB_URL"));
                System.out.println("✅ .env carregado com sucesso (modo desenvolvimento)");
            } else {
                System.out.println("ℹ️ .env não encontrado, usando variáveis de ambiente do sistema (modo produção)");
            }
        } catch (Exception e) {
            // No Render, usa as variáveis de ambiente do sistema
            System.out.println("ℹ️ .env não encontrado, usando variáveis de ambiente do sistema (modo produção)");
        }

        SpringApplication.run(IbaSistemaApplication.class, args);
    }
}