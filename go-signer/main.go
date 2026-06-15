// Command go-signer is an HTTP microservice that signs and verifies Football 501
// game results using ed25519 (Go standard library).
//
// Environment variables (set as Fly.io secrets in production):
//
//	SIGNING_KEY     — base64-encoded 64-byte ed25519 private key (required)
//	SIGNING_KEY_ID  — short label for this key, embedded in tokens (required)
//	PORT            — HTTP listen port, defaults to 8090
package main

import (
	"encoding/base64"
	"encoding/json"
	"errors"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/pauloneill/football-501/go-signer/signer"
)

func main() {
	s, err := loadSigner()
	if err != nil {
		log.Fatalf("cannot load signer: %v", err)
	}

	mux := http.NewServeMux()
	mux.HandleFunc("GET /health", handleHealth)
	mux.HandleFunc("GET /pubkey", handlePubKey(s))
	mux.HandleFunc("POST /sign", handleSign(s))
	mux.HandleFunc("POST /verify", handleVerify(s))

	port := os.Getenv("PORT")
	if port == "" {
		port = "8090"
	}

	log.Printf("go-signer listening on :%s", port)
	if err := http.ListenAndServe(":"+port, mux); err != nil {
		log.Fatalf("server error: %v", err)
	}
}

// loadSigner reads SIGNING_KEY and SIGNING_KEY_ID from the environment and
// constructs a Signer. Both variables are required.
func loadSigner() (*signer.Signer, error) {
	keyB64 := os.Getenv("SIGNING_KEY")
	if keyB64 == "" {
		return nil, errors.New("SIGNING_KEY env var is required")
	}
	keyBytes, err := base64.StdEncoding.DecodeString(keyB64)
	if err != nil {
		return nil, errors.New("SIGNING_KEY is not valid base64")
	}

	keyID := os.Getenv("SIGNING_KEY_ID")
	if keyID == "" {
		return nil, errors.New("SIGNING_KEY_ID env var is required")
	}

	return signer.New(keyBytes, keyID)
}

// ── Request / response types ──────────────────────────────────────────────────

type signRequest struct {
	GameID      string    `json:"gameId"`
	PlayerID    string    `json:"playerId"`
	FinalScore  int       `json:"finalScore"`
	CompletedAt time.Time `json:"completedAt"`
}

type signResponse struct {
	Token *signer.Token `json:"token"`
}

type verifyRequest struct {
	Token *signer.Token `json:"token"`
}

type verifyResponse struct {
	Valid   bool           `json:"valid"`
	Payload *signer.Payload `json:"payload,omitempty"`
	Error   string         `json:"error,omitempty"`
}

type pubKeyResponse struct {
	PublicKey string `json:"publicKey"` // base64-encoded ed25519 public key
	KeyID     string `json:"kid"`
}

type errorResponse struct {
	Error string `json:"error"`
}

// ── Handlers ──────────────────────────────────────────────────────────────────

func handleHealth(w http.ResponseWriter, _ *http.Request) {
	w.WriteHeader(http.StatusOK)
}

func handlePubKey(s *signer.Signer) http.HandlerFunc {
	// Pre-encode once at startup; public key never changes at runtime.
	encoded := base64.StdEncoding.EncodeToString(s.PublicKeyBytes())
	return func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, http.StatusOK, pubKeyResponse{PublicKey: encoded})
	}
}

func handleSign(s *signer.Signer) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var req signRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			writeJSON(w, http.StatusBadRequest, errorResponse{Error: "invalid JSON"})
			return
		}

		tok, err := s.Sign(signer.Payload{
			GameID:      req.GameID,
			PlayerID:    req.PlayerID,
			FinalScore:  req.FinalScore,
			CompletedAt: req.CompletedAt,
		})
		if err != nil {
			if errors.Is(err, signer.ErrMissingPayload) {
				writeJSON(w, http.StatusBadRequest, errorResponse{Error: err.Error()})
				return
			}
			log.Printf("sign error: %v", err)
			writeJSON(w, http.StatusInternalServerError, errorResponse{Error: "signing failed"})
			return
		}

		writeJSON(w, http.StatusOK, signResponse{Token: tok})
	}
}

func handleVerify(s *signer.Signer) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		var req verifyRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			writeJSON(w, http.StatusBadRequest, errorResponse{Error: "invalid JSON"})
			return
		}
		if req.Token == nil {
			writeJSON(w, http.StatusBadRequest, errorResponse{Error: "token is required"})
			return
		}

		payload, err := s.Verify(req.Token)
		if err != nil {
			writeJSON(w, http.StatusOK, verifyResponse{Valid: false, Error: err.Error()})
			return
		}

		writeJSON(w, http.StatusOK, verifyResponse{Valid: true, Payload: payload})
	}
}

// ── Helpers ───────────────────────────────────────────────────────────────────

func writeJSON(w http.ResponseWriter, status int, v any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if err := json.NewEncoder(w).Encode(v); err != nil {
		log.Printf("writeJSON encode error: %v", err)
	}
}
