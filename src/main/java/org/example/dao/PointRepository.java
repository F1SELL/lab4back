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
public class PointRepository extends BaseRepository<Point, Long> implements PointRepositoryInterface {

    @PersistenceContext
    protected EntityManager entityManager;

    public PointRepository() {
        super(Point.class);
    }

    @Override
    public List<Point> findByUser(User user) {
        return entityManager.createQuery("SELECT p FROM Point p WHERE p.user = ?1 ORDER BY p.currentTime DESC", Point.class)
                .setParameter(1, user)
                .getResultStream().toList();
    }

    @SuppressWarnings("unchecked")
    public List<Point> findPointsInBBox(User user, double minX, double minY, double maxX, double maxY) {
        String sql = "SELECT * FROM points p " +
                "WHERE p.user_id = ?1 " +
                "AND p.geom && ST_MakeEnvelope(?2, ?3, ?4, ?5, 4326)";

        return entityManager.createNativeQuery(sql, Point.class)
                .setParameter(1, user.getId())
                .setParameter(2, minX)
                .setParameter(3, minY)
                .setParameter(4, maxX)
                .setParameter(5, maxY)
                .getResultList();
    }

    public void updateGeom(Long pointId, double x, double y) {
        String sql = "UPDATE points SET geom = ST_SetSRID(ST_MakePoint(?1, ?2), 4326) WHERE id = ?3";
        entityManager.createNativeQuery(sql)
                .setParameter(1, x)
                .setParameter(2, y)
                .setParameter(3, pointId)
                .executeUpdate();
    }
}