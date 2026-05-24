import Sidebar from "@/components/admin/Sidebar";

export const metadata = {
  title: "Admin — Football 501",
};

export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="flex min-h-screen">
      <Sidebar />
      {/* Main content offset by sidebar width */}
      <main className="ml-[250px] flex-1 p-8 bg-[var(--color-background)] min-h-screen">
        {children}
      </main>
    </div>
  );
}
