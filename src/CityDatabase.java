import java.util.*;

public class CityDatabase {
    private static final Set<String> cities = new HashSet<>(Arrays.asList(
        "Москва", "Архангельск", "Киров", "Владимир", "Рязань",
        "Новосибирск", "Красноярск", "Калининград", "Дмитров", "Воронеж",
        "Жуковский", "Йошкар-Ола", "Астрахань", "Нижний Новгород", "Дубна",
        "Екатеринбург", "Гатчина", "Анапа", "Адлер", "Рыбинск"
    ));

    public static boolean isValidNextCity(String lastCity, String nextCity) {
        if (lastCity == null || lastCity.isEmpty()) {
            return cities.contains(nextCity);
        }
        
        char lastChar = Character.toLowerCase(lastCity.charAt(lastCity.length() - 1));
        // Особый случай для городов на ь, ъ
        if (lastChar == 'ь' || lastChar == 'ъ') {
            lastChar = lastCity.charAt(lastCity.length() - 2);
        }
        
        char firstChar = Character.toLowerCase(nextCity.charAt(0));
        return lastChar == firstChar && cities.contains(nextCity);
    }

    public static boolean isValidCity(String city) {
        return cities.contains(city);
    }

    public static Set<String> getAllCities() {
        return new HashSet<>(cities);
    }
} 