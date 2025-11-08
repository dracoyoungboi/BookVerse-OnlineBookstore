package com.bookverse.BookVerse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class EncodeTest {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println(encoder.encode("123456"));
    }
}

