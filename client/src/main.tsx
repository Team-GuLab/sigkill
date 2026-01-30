import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import Router from "./routes";
import TanstackQueryClientProvider from "./app/provider/tanstack-query-client-provider";
import { enableMocking } from "./_mocks";

enableMocking().then(() => {
  createRoot(document.getElementById("root")!).render(
    <StrictMode>
      <TanstackQueryClientProvider>
        <Router />
      </TanstackQueryClientProvider>
    </StrictMode>,
  );
});
