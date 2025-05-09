public class CityDatabase {
    public static boolean isValidNextCity(String lastCity, String nextCity) {
        if (lastCity == null || lastCity.isEmpty()) {
            return true; // Первый город всегда валиден
        }
        
        if (nextCity == null || nextCity.isEmpty()) {
            return false;
        }
        
        // Приводим к нижнему регистру для сравнения
        lastCity = lastCity.toLowerCase();
        nextCity = nextCity.toLowerCase();
        
        // Получаем последнюю букву предыдущего города
        char lastLetter = lastCity.charAt(lastCity.length() - 1);
        
        // Если последняя буква - ь, ъ, ы, й, берем предпоследнюю
        if (lastLetter == 'ь' || lastLetter == 'ъ' || lastLetter == 'ы' || lastLetter == 'й') {
            if (lastCity.length() > 1) {
                lastLetter = lastCity.charAt(lastCity.length() - 2);
            }
        }
        
        // Получаем первую букву следующего города
        char firstLetter = nextCity.charAt(0);
        
        return lastLetter == firstLetter;
    }
} 