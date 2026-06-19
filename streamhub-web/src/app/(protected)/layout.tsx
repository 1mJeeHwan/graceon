import type { ReactNode } from "react";
import { redirect } from "next/navigation";

import { auth } from "@/../auth";
import Sidebar from "@/components/layout/Sidebar";
import Header from "@/components/layout/Header";

export default async function ProtectedLayout({
  children,
}: {
  children: ReactNode;
}) {
  // Defense-in-depth: middleware also guards these routes, but verify the
  // session server-side so a middleware bypass (e.g. CVE-2025-29927) cannot
  // expose the admin chrome to unauthenticated users.
  const session = await auth();
  if (!session) {
    redirect("/login");
  }

  return (
    <div className="flex h-screen overflow-hidden bg-slate-50">
      <Sidebar />
      <div className="flex flex-1 flex-col overflow-hidden">
        <Header />
        <main className="flex-1 overflow-y-auto p-6">{children}</main>
      </div>
    </div>
  );
}
