package com.example.springboot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

@SpringBootApplication
public class Application implements CommandLineRunner {

    @Autowired
    JdbcTemplate jdbcTemplate;

    private Map<String, Double> stocksList;
    private Method executeQuery;

    public static void main(String args[]) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public WebMvcConfigurer configure() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry reg) {
                reg.addMapping("/**").allowedOrigins("*");
            }
        };
    }

    @Override
    public void run(String... strings) {

         //updateQuotes();

        try {
            startKdbClient();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void startKdbClient() throws IOException, RuntimeException {
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(isr);

        while (true) {
            try {
                System.out.print("q) ");
                String sql = "q)" + br.readLine();
                jdbcTemplate.query(sql, (ResultSet rowSet) -> {
                    ResultSetMetaData m;
                    m = rowSet.getMetaData();
                    int columnCount = m.getColumnCount();

                    String fmt[] = new String[columnCount + 1];
                    int width = 0;

                    for (int i = 1; i <= columnCount; i++) {
                        fmt[i] = "%-" + m.getColumnDisplaySize(i) + "s";
                        System.out.format(fmt[i], m.getColumnLabel(i));
                        width += rowSet.getMetaData().getColumnDisplaySize(i);
                    }

                    System.out.print("\n");

                    for (int i = 1; i <= width; i++) {
                        System.out.print("-");
                    }

                    System.out.print("\n");

                    do {
                        for (int i = 1; i <= columnCount; i++) {
                            System.out.format(fmt[i], rowSet.getString(i));
                        }
                        System.out.print("\n");
                        if (rowSet.getRow() > 10) {
                            System.out.println("...");
                            rowSet.afterLast();
                        }
                    } while (rowSet.next());
                });
            } catch (IOException e) {
                e.printStackTrace();
            } catch (DataAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void updateQuotes() {
        stocksList = getStockList();
        try {
            executeQuery = Class.forName("jdbc$co").getMethod("ex", String.class, Object[].class);
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            System.err.println("kdb+ JDBC driver has not loaded properly");
            e.printStackTrace();
            System.exit(1);
        }

        // Insert for infinite time, updates the value in stocks from stocks.list
        while (true) {
            try (Connection c = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection()) {
                // System.out.format("upd:{[arg] t:arg[0]; x:arg[1]; z:(count x)#.z.T;t insert (enlist z),flip x; }");
                executeQuery.invoke(c.getMetaData().getConnection(), "upd", new Object[]{"quote", getDummyData()});
                Thread.sleep(1000);
            } catch (IllegalAccessException | InvocationTargetException | InterruptedException | SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private Object[] getDummyData() {
        List<Object[]> l = new ArrayList<>();
        stocksList.forEach((k, v) -> l.add(new Object[]{k, v - 2 * Math.round(100 * Math.random()) / 100.0, v + 2 * Math.round(100 * Math.random()) / 100.0}));
        return l.toArray();
    }

    private Map<String, Double> getStockList() {
        Map<String, Double> ret = new HashMap<>();
        try {
            Properties props = new Properties();
            FileInputStream in = new FileInputStream(new ClassPathResource("stocks.list").getFile());
            props.load(in);
            props.forEach((k, v) -> ret.put((String) k, Double.parseDouble((String) v)));
            in.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }
}