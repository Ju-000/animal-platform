export const featuredAnimals = [
  {
    id: 1,
    name: "보리",
    species: "강아지",
    shelter: "서울 마포보호센터",
    shelterId: 1,
    age: "2살 추정",
    sex: "암컷",
    weight: "6.1kg",
    foundPlace: "서울 마포구",
    status: "입양 가능",
    summary: "사람을 잘 따르고 교감이 좋은 아이"
  },
  {
    id: 2,
    name: "콩이",
    species: "고양이",
    shelter: "경기 수원쉼터",
    shelterId: 2,
    age: "1살 추정",
    sex: "수컷",
    weight: "3.8kg",
    foundPlace: "경기 수원시",
    status: "후원 필요",
    summary: "건강 관리 중이며 조용한 환경을 좋아하는 아이"
  },
  {
    id: 3,
    name: "해달",
    species: "강아지",
    shelter: "부산 해운대센터",
    shelterId: 3,
    age: "3살 추정",
    sex: "수컷",
    weight: "9.4kg",
    foundPlace: "부산 해운대구",
    status: "관심 집중",
    summary: "활발하고 산책을 좋아해 가족 적응력이 좋은 아이"
  }
];

export const shelterCards = [
  {
    id: 1,
    name: "서울 마포보호센터",
    region: "서울",
    address: "서울시 마포구 새빛로 12",
    phone: "02-111-2222",
    animals: 24,
    donations: "8,400,000원"
  },
  {
    id: 2,
    name: "경기 수원쉼터",
    region: "경기",
    address: "경기도 수원시 장안로 20",
    phone: "031-222-3333",
    animals: 17,
    donations: "5,100,000원"
  },
  {
    id: 3,
    name: "부산 해운대센터",
    region: "부산",
    address: "부산시 해운대구 바다길 33",
    phone: "051-333-4444",
    animals: 31,
    donations: "10,700,000원"
  }
];

export const statCards = [
  { label: "현재 보호 동물", value: "128" },
  { label: "이번 달 입양 신청", value: "34" },
  { label: "누적 후원 금액", value: "24,200,000원" },
  { label: "연계 보호소", value: "27" }
];

export function getAnimalById(id) {
  return featuredAnimals.find((animal) => String(animal.id) === String(id));
}

export function getShelterById(id) {
  return shelterCards.find((shelter) => String(shelter.id) === String(id));
}

export function getAnimalsByShelterId(shelterId) {
  return featuredAnimals.filter((animal) => String(animal.shelterId) === String(shelterId));
}