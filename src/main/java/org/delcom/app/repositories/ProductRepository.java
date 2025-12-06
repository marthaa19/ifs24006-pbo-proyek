package org.delcom.app.repositories;

import org.delcom.app.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    // Mencari berdasarkan field 'userId' yang baru kita buat
    List<Product> findByUserIdOrderByCreatedAtDesc(UUID userId);
}