:introduction

Grant a user access to a product. **NB!** While using `GET` with this endpoint
will also provide information about access to subscriptions, `POST` can only be
used to grant access to products, not subscriptions.

:relevant-endpoints

GET /user/{userId}/products
GET /user/{userId}/product/{productId}
DELETE /user/{userId}/product/{productId}
