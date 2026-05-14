export async function requestPortOnePayment({
  portOneConfig,
  paymentRequest
}) {
  if (!portOneConfig?.enabled) {
    throw new Error("포트원 설정이 아직 비어 있습니다. 환경 변수를 먼저 넣어주세요.");
  }

  if (!window.IMP) {
    throw new Error("포트원 SDK 스크립트를 추가한 뒤 window.IMP 초기화를 연결해주세요.");
  }

  const { IMP } = window;
  IMP.init(portOneConfig.storeId);

  return new Promise((resolve, reject) => {
    IMP.request_pay(
      {
        pg: portOneConfig.channelKey || undefined,
        merchant_uid: paymentRequest.merchantUid,
        name: paymentRequest.orderName,
        amount: Number(paymentRequest.amount),
        buyer_name: paymentRequest.buyerName,
        buyer_email: paymentRequest.buyerEmail,
        buyer_tel: paymentRequest.buyerTel
      },
      (response) => {
        if (response?.success) {
          resolve(response);
          return;
        }

        reject(new Error(response?.error_msg || "결제에 실패했습니다."));
      }
    );
  });
}