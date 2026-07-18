import { Toaster } from "sonner";
import { AppProvider, useApp } from "./contexts/AppContext";
import { Layout } from "./components/layout/Layout";
import { Dashboard } from "./pages/Dashboard";
import { RiskEvents } from "./pages/RiskEvents";
import { EventDetail } from "./pages/EventDetail";
import { RiskZones } from "./pages/RiskZones";
import { Trips } from "./pages/Trips";
import { Devices } from "./pages/Devices";
import { AIAnalysis } from "./pages/AIAnalysis";
import { Reports } from "./pages/Reports";
import { Settings } from "./pages/Settings";
import { MyPage } from "./pages/MyPage";

function AppContent() {
  const { page } = useApp();

  const renderPage = () => {
    switch (page) {
      case "dashboard":    return <Dashboard />;
      case "events":       return <RiskEvents />;
      case "event-detail": return <EventDetail />;
      case "zones":        return <RiskZones />;
      case "trips":        return <Trips />;
      case "devices":      return <Devices />;
      case "ai":           return <AIAnalysis />;
      case "reports":      return <Reports />;
      case "settings":     return <Settings />;
      case "mypage":       return <MyPage />;
      default:             return <Dashboard />;
    }
  };

  return <Layout>{renderPage()}</Layout>;
}

export default function App() {
  return (
    <AppProvider>
      <Toaster position="top-right" richColors />
      <AppContent />
    </AppProvider>
  );
}
