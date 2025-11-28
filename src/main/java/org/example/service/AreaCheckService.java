package org.example.service;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import org.example.dao.PointRepository;
import org.example.dto.CheckRequest;
import org.example.entity.Point;
import org.example.entity.User;
import org.example.exception.ValidationException;
import org.example.tools.ValidationAnswer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Stateless
public class AreaCheckService {

    private final Set<Double> validXValues = Set.of(-3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0);
    private final Set<Double> validRValues = Set.of(1.0, 2.0, 3.0, 4.0, 5.0);

    @EJB
    private PointRepository pointRepository;

    public Point checkPoint(CheckRequest request, User user) {
        Long startTime = System.currentTimeMillis();
        ValidationAnswer answer = validate(request);
        if (!answer.status()) throw ValidationException.withDescription(answer.parameter(), answer.message());
        boolean hit = checkHit(request.getX(), request.getY(), request.getR());
        Point point = Point.builder()
                .x(request.getX())
                .y(request.getY())
                .r(request.getR())
                .hit(hit)
                .currentTime(LocalDateTime.now())
                .executionTime(System.currentTimeMillis() - startTime)
                .user(user)
                .build();
        return pointRepository.save(point);
    }

    private ValidationAnswer validate(CheckRequest params) {
        try {
            if (params.getR() == null) {
                return new ValidationAnswer(false, "r", "отсутствует или не корректен");
            }
            if (params.getX() == null) {
                return new ValidationAnswer(false, "x", "отсутствует или не корректен");
            }
            if (params.getY() == null) {
                return new ValidationAnswer(false, "y", "отсутствует или не корректен");
            }

            boolean validX = false;
            for (Double num : validXValues) {
                BigDecimal validXBigDecimal = BigDecimal.valueOf(num);
                if (params.getX().compareTo(validXBigDecimal) == 0) {
                    validX = true;
                    break;
                }
            }
            if (!validX) {
                return new ValidationAnswer(false, "x", "должен быть из: " + validXValues);
            }

            boolean validR = false;
            for (Double num : validRValues) {
                if (num.equals(params.getR().doubleValue())) {
                    validR = true;
                    break;
                }
            }
            if (!validR) {
                return new ValidationAnswer(false, "r", "должен быть из: " + validRValues + " и > 0");
            }

            if (params.getR() <= 0) {
                return new ValidationAnswer(false, "r", "должен быть строго больше 0");
            }

            BigDecimal minusFive = new BigDecimal("-5");
            BigDecimal five = new BigDecimal("5");
            if (params.getY().compareTo(minusFive) < 0 || params.getY().compareTo(five) > 0) {
                return new ValidationAnswer(false, "y", "должен быть от -5 до 5. Текущее: " + params.getY());
            }

            return new ValidationAnswer(true, null, null);

        } catch (Exception ex) {
            return new ValidationAnswer(false, "error", "Ошибка валидации: " + ex.getMessage());
        }
    }

    private boolean checkHit(BigDecimal x, BigDecimal y, Float rF) {
        BigDecimal zero = BigDecimal.ZERO;
        BigDecimal r = BigDecimal.valueOf(rF);
        BigDecimal rHalf = r.divide(BigDecimal.valueOf(2));

        if (x.compareTo(zero) >= 0 && y.compareTo(zero) >= 0) {
            boolean inTriangleBounds = x.compareTo(rHalf) <= 0 && y.compareTo(r) <= 0;
            if (inTriangleBounds) {
                BigDecimal lineValue = x.multiply(BigDecimal.valueOf(2)).add(y);
                if (lineValue.compareTo(r) <= 0) {
                    return true;
                }
            }
        }

        if (x.compareTo(zero) <= 0 && y.compareTo(zero) >= 0) {
            boolean inRectangle = x.compareTo(rHalf.negate()) >= 0 &&
                    y.compareTo(r) <= 0;
            if (inRectangle) {
                return true;
            }
        }

        if (x.compareTo(zero) <= 0 && y.compareTo(zero) <= 0) {
            BigDecimal distanceSquared = x.multiply(x).add(y.multiply(y));
            BigDecimal rSquared = r.multiply(r);
            if (distanceSquared.compareTo(rSquared) <= 0) {
                return true;
            }
        }

        return false;
    }
}