package org.example.dao;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.entity.Point;
import org.example.entity.User;

import java.util.List;

@Stateless
@LocalBean
public class PointRepository extends BaseRepository<Point, Long> implements PointRepositoryInterface{

    @PersistenceContext
    protected EntityManager entityManager;


    public PointRepository(){
        super(Point.class);
    }

    @Override
    public List<Point> findByUser(User user) {
        return entityManager.createQuery("SELECT p FROM Point p WHERE p.user = ?1 ORDER BY p.currentTime", Point.class)
                .setParameter(1, user)
                .getResultStream().toList();
    }
}
