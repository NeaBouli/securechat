/**
 * StealthX Platform — App Signature Verifier Middleware
 * Identical pattern to SecureCall server.js implementation.
 * Protects against forks using their own servers.
 */

const ALLOWED_SIGNATURES = process.env.ALLOWED_SIGNATURES
    ? process.env.ALLOWED_SIGNATURES.split(',').map(s => s.trim().toLowerCase())
    : null;

function verifyAppSignature(req, res, next) {
    if (!ALLOWED_SIGNATURES || ALLOWED_SIGNATURES.length === 0) {
        return next();
    }

    const signature = (req.headers['x-app-signature'] || req.body?.appSignature || '').toLowerCase();

    if (!signature || !ALLOWED_SIGNATURES.includes(signature)) {
        console.warn(`[SECURITY] Rejected connection — signature: ${signature}`);
        return res.status(403).json({
            error: 'unauthorized_client',
            message: 'App signature not recognized.'
        });
    }

    next();
}

function verifyWsSignature(ws, req, next) {
    if (!ALLOWED_SIGNATURES || ALLOWED_SIGNATURES.length === 0) {
        return next();
    }

    const signature = (req.headers['x-app-signature'] || '').toLowerCase();

    if (!signature || !ALLOWED_SIGNATURES.includes(signature)) {
        console.warn(`[SECURITY] Rejected WS — signature: ${signature}`);
        ws.send(JSON.stringify({ type: 'ERROR', error: 'unauthorized_client' }));
        ws.close();
        return;
    }

    next();
}

module.exports = { verifyAppSignature, verifyWsSignature };
