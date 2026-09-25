const jwt = require('jsonwebtoken');
const jwksClient = require('jwks-rsa');

const USER_POOL_ID = process.env.COGNITO_USER_POOL_ID;
const REGION = process.env.COGNITO_REGION;
const CLIENT_ID = process.env.COGNITO_CLIENT_ID;
const ISSUER = process.env.COGNITO_ISSUER || `https://cognito-idp.${REGION}.amazonaws.com/${USER_POOL_ID}`;
const JWKS_URI = process.env.COGNITO_JWKS_URI || `${ISSUER}/.well-known/jwks.json`;

const client = jwksClient({
  jwksUri: JWKS_URI,
  cache: true,
  cacheMaxAge: 10 * 60 * 1000,
  rateLimit: true,
});

function getSigningKey(kid) {
  return new Promise((resolve, reject) => {
    client.getSigningKey(kid, (err, key) => {
      if (err) return reject(err);
      resolve(key.getPublicKey());
    });
  });
}

function extractToken(event) {
  const authHeader =
    event.headers?.authorization ||
    event.headers?.Authorization ||
    event.identitySource?.[0];

  if (!authHeader) return null;
  const parts = authHeader.split(' ');
  if (parts.length === 2 && parts[0].toLowerCase() === 'bearer') {
    return parts[1];
  }
  return authHeader;
}

function deny(message) {
  console.warn('Autorización rechazada:', message);
  return { isAuthorized: false };
}

exports.handler = async (event) => {
  try {
    if (!USER_POOL_ID || !CLIENT_ID) {
      return deny('COGNITO_USER_POOL_ID o COGNITO_CLIENT_ID no configurados en el Lambda.');
    }

    const token = extractToken(event);
    if (!token) return deny('No llegó Authorization: Bearer <token>.');

    const decodedHeader = jwt.decode(token, { complete: true });
    if (!decodedHeader?.header?.kid) return deny('Token sin kid en el header.');

    const publicKey = await getSigningKey(decodedHeader.header.kid);

    // Se valida el ID token (no el access token): trae el claim "aud" estándar y
    // el listado de grupos ("cognito:groups") con el rol del usuario, que es lo
    // que necesita el BFF para autorizar — un access token de Cognito no incluye
    // ninguna de las dos cosas por defecto.
    const claims = jwt.verify(token, publicKey, {
      algorithms: ['RS256'],
      issuer: ISSUER,
      audience: CLIENT_ID,
    });

    if (claims.token_use !== 'id') {
      return deny(`Se esperaba un ID token, llegó token_use="${claims.token_use}".`);
    }

    const roles = claims['cognito:groups'] || [];

    return {
      isAuthorized: true,
      context: {
        userId: claims.sub,
        email: claims.email || '',
        roles: roles.join(','),
      },
    };
  } catch (err) {
    return deny(err.message);
  }
};
