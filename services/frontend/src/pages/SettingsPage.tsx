import { useState, type FormEvent } from "react";
import { ApiError, useChangePassword, useCurrentUser, useUpdateProfile, type User } from "@/api/queries";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

function ProfileCard({ user }: { user: User }) {
  const updateProfile = useUpdateProfile();
  const [displayName, setDisplayName] = useState(user.displayName);
  const [email, setEmail] = useState(user.email);
  const [currentPassword, setCurrentPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  const emailChanged = email.trim().toLowerCase() !== user.email;
  const nameChanged = displayName.trim() !== user.displayName && displayName.trim().length > 0;
  const dirty = emailChanged || nameChanged;

  function onFieldChange() {
    setSaved(false);
    setError(null);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await updateProfile.mutateAsync({
        displayName: nameChanged ? displayName.trim() : undefined,
        email: emailChanged ? email.trim() : undefined,
        currentPassword: emailChanged ? currentPassword : undefined,
      });
      setCurrentPassword("");
      setSaved(true);
    } catch (err) {
      // The gateway proxy strips downstream error bodies, so map by status.
      if (err instanceof ApiError && err.status === 409) {
        setError("This email is already registered");
      } else if (err instanceof ApiError && err.status === 401) {
        setError("Wrong password");
      } else {
        setError(err instanceof Error ? err.message : "Profile update failed");
      }
    }
  }

  return (
    <Card>
      <CardHeader className="pb-4">
        <CardTitle className="text-lg">Profile</CardTitle>
        <CardDescription>Your name and email address</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="displayName">Name</Label>
            <Input id="displayName" type="text" value={displayName} onChange={(e) => { setDisplayName(e.target.value); onFieldChange(); }} required maxLength={100} autoComplete="name" />
          </div>
          <div className="space-y-2">
            <Label htmlFor="email">Email</Label>
            <Input id="email" type="email" value={email} onChange={(e) => { setEmail(e.target.value); onFieldChange(); }} required autoComplete="email" />
          </div>
          {emailChanged && (
            <div className="space-y-2">
              <Label htmlFor="profileCurrentPassword">Current password</Label>
              <Input id="profileCurrentPassword" type="password" placeholder="Required to change your email" value={currentPassword} onChange={(e) => { setCurrentPassword(e.target.value); onFieldChange(); }} required autoComplete="current-password" />
            </div>
          )}

          {error && <p className="text-sm text-destructive">{error}</p>}
          {saved && !dirty && <p className="text-sm text-green-600">Saved</p>}

          <Button type="submit" disabled={!dirty || updateProfile.isPending}>
            {updateProfile.isPending ? "Saving…" : "Save changes"}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}

function PasswordCard() {
  const changePassword = useChangePassword();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSaved(false);
    if (newPassword !== confirmPassword) {
      setError("New passwords do not match");
      return;
    }
    try {
      await changePassword.mutateAsync({ currentPassword, newPassword });
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setSaved(true);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        setError("Wrong password");
      } else {
        setError(err instanceof Error ? err.message : "Password change failed");
      }
    }
  }

  return (
    <Card>
      <CardHeader className="pb-4">
        <CardTitle className="text-lg">Password</CardTitle>
        <CardDescription>Other signed-in sessions stay active until they expire</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="currentPassword">Current password</Label>
            <Input id="currentPassword" type="password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)} required autoComplete="current-password" />
          </div>
          <div className="space-y-2">
            <Label htmlFor="newPassword">New password</Label>
            <Input id="newPassword" type="password" placeholder="At least 8 characters" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required minLength={8} maxLength={128} autoComplete="new-password" />
          </div>
          <div className="space-y-2">
            <Label htmlFor="confirmPassword">Confirm new password</Label>
            <Input id="confirmPassword" type="password" value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} required minLength={8} maxLength={128} autoComplete="new-password" />
          </div>

          {error && <p className="text-sm text-destructive">{error}</p>}
          {saved && <p className="text-sm text-green-600">Password changed</p>}

          <Button type="submit" disabled={changePassword.isPending}>
            {changePassword.isPending ? "Changing…" : "Change password"}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}

export default function SettingsPage() {
  const { data: user } = useCurrentUser();

  return (
    <div className="flex flex-col h-full">
      {/* Page header */}
      <header className="border-b bg-white px-6 py-4">
        <h1 className="text-xl font-semibold">Settings</h1>
        <p className="text-sm text-muted-foreground">Manage your account</p>
      </header>

      <div className="flex-1 overflow-y-auto p-6">
        <div className="max-w-lg space-y-6">
          {user ? (
            // Key by user id so the form state resets if the account changes.
            <ProfileCard key={user.id} user={user} />
          ) : (
            <Skeleton className="h-64 w-full" />
          )}
          <PasswordCard />
        </div>
      </div>
    </div>
  );
}
