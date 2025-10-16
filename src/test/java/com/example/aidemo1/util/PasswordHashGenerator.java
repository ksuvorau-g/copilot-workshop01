package com.example.aidemo1.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class to generate BCrypt password hashes for test users.
 * Run this class to generate hashes for migration files.
 */
public class PasswordHashGenerator {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        System.out.println("Generating BCrypt password hashes...\n");
        
        String userPassword = "user";
        String adminPassword = "admin";
        String premPassword = "prem";
        
        System.out.println("user/user:");
        System.out.println(encoder.encode(userPassword));
        System.out.println();
        
        System.out.println("admin/admin:");
        System.out.println(encoder.encode(adminPassword));
        System.out.println();
        
        System.out.println("prem/prem:");
        System.out.println(encoder.encode(premPassword));
    }
}
