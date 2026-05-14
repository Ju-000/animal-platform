import { Route, Routes } from "react-router-dom";
import Layout from "./components/Layout";
import HomePage from "./pages/HomePage";
import AnimalsPage from "./pages/AnimalsPage";
import AnimalDetailPage from "./pages/AnimalDetailPage";
import SheltersPage from "./pages/SheltersPage";
import ShelterDetailPage from "./pages/ShelterDetailPage";
import DonationsPage from "./pages/DonationsPage";
import FavoritesPage from "./pages/FavoritesPage";
import StatsPage from "./pages/StatsPage";
import StoryPage from "./pages/StoryPage";
import StoriesPage from "./pages/StoriesPage";
import StoryDetailPage from "./pages/StoryDetailPage";
import StoryWritePage from "./pages/StoryWritePage";
import LoginPage from "./pages/LoginPage";
import LoginSuccessPage from "./pages/LoginSuccessPage";
import LoginFailurePage from "./pages/LoginFailurePage";
import MyPage from "./pages/MyPage";
import ShelterAdminPage from "./pages/ShelterAdminPage";
import AdminPage from "./pages/AdminPage";
import AdminLayout from "./components/admin/AdminLayout";
import CampaignManagePage from "./pages/admin/CampaignManagePage";
import AdoptionManagePage from "./pages/admin/AdoptionManagePage";
import DonationManagePage from "./pages/admin/DonationManagePage";
import UserManagePage from "./pages/admin/UserManagePage";
import BatchHistoryPage from "./pages/admin/BatchHistoryPage";
import MatchingPage from "./pages/MatchingPage";
import ErrorBoundary from "./components/common/ErrorBoundary";
import PageFallback from "./components/common/PageFallback";

function withPageBoundary(page) {
  return (
    <ErrorBoundary fallback={<PageFallback />}>
      {page}
    </ErrorBoundary>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={withPageBoundary(<HomePage />)} />
        <Route path="animals" element={withPageBoundary(<AnimalsPage />)} />
        <Route path="animals/:animalSlug" element={withPageBoundary(<AnimalDetailPage />)} />
        <Route path="shelter/animal/detail/:animalSlug" element={withPageBoundary(<AnimalDetailPage />)} />
        <Route path="shelters" element={withPageBoundary(<SheltersPage />)} />
        <Route path="shelters/:shelterId" element={withPageBoundary(<ShelterDetailPage />)} />
        <Route path="donation" element={withPageBoundary(<DonationsPage />)} />
        <Route path="donations" element={withPageBoundary(<DonationsPage />)} />
        <Route path="favorites" element={withPageBoundary(<FavoritesPage />)} />
        <Route path="matching" element={withPageBoundary(<MatchingPage />)} />
        <Route path="stats" element={withPageBoundary(<StatsPage />)} />
        <Route path="story" element={withPageBoundary(<StoryPage />)} />
        <Route path="stories" element={withPageBoundary(<StoriesPage />)} />
        <Route path="stories/write" element={withPageBoundary(<StoryWritePage />)} />
        <Route path="stories/:storyId" element={withPageBoundary(<StoryDetailPage />)} />
        <Route path="login" element={withPageBoundary(<LoginPage />)} />
        <Route path="login/success" element={withPageBoundary(<LoginSuccessPage />)} />
        <Route path="login/failure" element={withPageBoundary(<LoginFailurePage />)} />
        <Route path="me" element={withPageBoundary(<MyPage />)} />
        <Route path="shelter-admin" element={withPageBoundary(<ShelterAdminPage />)} />
        <Route path="admin" element={withPageBoundary(<AdminLayout />)}>
          <Route index element={<AdminPage />} />
          <Route path="adoptions" element={<AdoptionManagePage />} />
          <Route path="donations" element={<DonationManagePage />} />
          <Route path="campaigns" element={<CampaignManagePage />} />
          <Route path="users" element={<UserManagePage />} />
          <Route path="batch" element={<BatchHistoryPage />} />
        </Route>
      </Route>
    </Routes>
  );
}
