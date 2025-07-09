import 'package:check_licence_tech/check_licence_tech.dart';

import 'constants/licence_constants.dart';

class LicenceService {
  Future<bool> checkLicence({
    required String licenceKey,
    String? url,
  }) async {
    // Simulate a licence check
    // In a real implementation, this would involve an API call or similar
    if (licenceKey.isEmpty) {
      return false; // Invalid licence key
    }

    final licence = CheckLicenceTech();
    final resultLicence = await licence.checkLicence(
      licenceKey,
      url: licenceCheck,
    );
    if (!resultLicence) {
      throw Exception('Invalid licence key. Please check your licence key.');
    }

    // Simulate a successful licence check
    return resultLicence;
  }
}
