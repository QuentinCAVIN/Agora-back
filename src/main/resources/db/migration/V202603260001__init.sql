-- Migration initiale : table minimale pour valider le démarrage Flyway
-- TODO: remplacer par le schéma complet à l'étape modèle métier

CREATE TABLE users (
    id    UUID         PRIMARY KEY,
    email VARCHAR(255) NOT NULL
);
