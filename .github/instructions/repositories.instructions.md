---
applyTo: '**/*Repository.java'
---

- **Spring Data Repository Query Methods**: Always prefer Spring Data Derived Query Methods over `@Query` annotations. Derived query methods (e.g., `findByUsername`, `existsByCode`, `deleteByName`) are automatically implemented by Spring Data from the method name. Only use `@Query` annotations when the query logic is too complex to express with a derived method name (e.g., complex joins, subqueries, or specific performance optimizations).

- DO NOT add comments about Derived Query Methods or why you chosen to use @Query annotation.