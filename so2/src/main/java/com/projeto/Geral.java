package com.projeto;

import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class Geral {
    public static Semaphore mutex = new Semaphore(1);
    public static ArrayList<Semaphore> recursos = new ArrayList<>();
    public static ArrayList<String> nomesRecursos = new ArrayList<>();
}