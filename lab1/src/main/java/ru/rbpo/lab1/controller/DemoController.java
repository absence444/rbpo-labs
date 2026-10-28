package ru.rbpo.lab1.controller;

import java.util.List;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DemoController {

    private final String applicationName;

    public DemoController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    /** Возвращает текстовое приветствие. */
    @GetMapping("/hello")
    public String hello(@RequestParam(defaultValue = "World") String name) {
        return "Hello, " + name + "! Application: " + applicationName;
    }

    /** Возвращает первые {@code count} квадратов натуральных чисел. */
    @GetMapping("/squares")
    public List<Integer> squares(@RequestParam(defaultValue = "5") int count) {
        int limited = Math.max(0, Math.min(count, 100));
        return IntStream.rangeClosed(1, limited).map(i -> i * i).boxed().toList();
    }
}
