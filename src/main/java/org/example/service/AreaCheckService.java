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

    private final Set<Double> validRValues = Set.of(0.0, 1.0, 2.0, 3.0, 4.0, 5.0);

    @EJB
    private PointRepository pointRepository;

    public Point checkPoint(CheckRequest request, User user) {
        Long startTime = System.nanoTime();

        ValidationAnswer answer = validate(request);
        if (!answer.status()) throw ValidationException.withDescription(answer.parameter(), answer.message());

        boolean hit = checkHit(request.getX(), request.getY(), request.getR());

        Point point = Point.builder()
                .x(request.getX())
                .y(request.getY())
                .r(request.getR())
                .hit(hit)
                .currentTime(LocalDateTime.now())
                .executionTime((System.nanoTime() - startTime) / 1000)
                .user(user)
                .build();

        Point savedPoint = pointRepository.save(point);

        pointRepository.updateGeom(
                savedPoint.getId(),
                request.getX().doubleValue(),
                request.getY().doubleValue()
        );

        return savedPoint;
    }

    private ValidationAnswer validate(CheckRequest params) {
        try {
            if (params.getR() == null) return new ValidationAnswer(false, "r", "отсутствует");
            if (params.getX() == null) return new ValidationAnswer(false, "x", "отсутствует");
            if (params.getY() == null) return new ValidationAnswer(false, "y", "отсутствует");

            boolean validR = false;
            for (Double num : validRValues) {
                if (num.equals(params.getR().doubleValue())) {
                    validR = true;
                    break;
                }
            }
            if (!validR) {
                return new ValidationAnswer(false, "r", "должен быть из: " + validRValues);
            }
            if (params.getR() < 0) {
                return new ValidationAnswer(false, "r", "не может быть отрицательным");
            }

            return new ValidationAnswer(true, null, null);

        } catch (Exception ex) {
            return new ValidationAnswer(false, "error", "Ошибка валидации: " + ex.getMessage());
        }
    }

    private boolean checkHit(BigDecimal x, BigDecimal y, Float rF) {
        if (rF == 0) return false;

        BigDecimal r = BigDecimal.valueOf(rF);
        BigDecimal zero = BigDecimal.ZERO;
        BigDecimal rHalf = r.multiply(BigDecimal.valueOf(0.5));

        if (x.compareTo(zero) >= 0 && y.compareTo(zero) >= 0) {
            BigDecimal twoX = x.multiply(BigDecimal.valueOf(2));
            return twoX.add(y).compareTo(r) <= 0;
        }

        if (x.compareTo(zero) <= 0 && y.compareTo(zero) >= 0) {
            return x.compareTo(rHalf.negate()) >= 0 && y.compareTo(r) <= 0;
        }

        if (x.compareTo(zero) <= 0 && y.compareTo(zero) <= 0) {
            return false;
        }

        if (x.compareTo(zero) >= 0 && y.compareTo(zero) <= 0) {
            BigDecimal distSq = x.pow(2).add(y.pow(2));
            BigDecimal rHalfSq = rHalf.pow(2);
            return distSq.compareTo(rHalfSq) <= 0;
        }

        return false;
    }
}