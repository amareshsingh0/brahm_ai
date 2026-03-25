import * as Sentry from "@sentry/react";
import { createRoot } from "react-dom/client";
import App from "./App.tsx";
import "./index.css";
import "./i18n";

Sentry.init({
  dsn: "https://6679165a1a3bbb1dc1440ce41c74ee53@o4511104916389888.ingest.us.sentry.io/4511105062207488",
  integrations: [Sentry.browserTracingIntegration()],
  tracesSampleRate: 0.2,
  environment: import.meta.env.MODE, // "development" or "production"
  enabled: import.meta.env.PROD,     // sirf production mein active
});

createRoot(document.getElementById("root")!).render(<App />);
