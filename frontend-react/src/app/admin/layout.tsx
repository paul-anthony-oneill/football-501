import Sidebar from "@/components/admin/Sidebar";
import ErrorBoundary from "@/components/ErrorBoundary";

export const metadata = {
  title: "Admin — Trivia 501",
};

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <ErrorBoundary section="admin">
      <div className="flex min-h-screen">
        <Sidebar />
        {/* Main content offset by sidebar width (set via CSS custom property) */}
        <main
          className="flex-1 p-8 bg-[var(--color-background)] min-h-screen transition-all duration-300"
          style={{ marginLeft: "var(--sidebar-width, 250px)" }}
        >
          {children}
        </main>
      </div>
    </ErrorBoundary>
  );
}
