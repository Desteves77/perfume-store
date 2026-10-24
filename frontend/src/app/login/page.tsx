"use client";
import { Suspense } from "react";
import { LoginForm } from "@/components/auth-form";
export default function LoginPage() { return <div className="page container" style={{ maxWidth: 620 }}><Suspense><LoginForm /></Suspense></div>; }
